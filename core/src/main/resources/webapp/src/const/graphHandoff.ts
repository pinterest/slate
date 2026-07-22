import { utils } from '@rjsf/core';
import { INodeData, TNode, IResourceDefinitions } from './types';
import { NEW_NODE_ID_PREFIX } from './constants';

/**
 * Agent -> Builder handoff.
 *
 * An agent can hand a user a create-only graph by encoding the same
 * `Record<resourceId, resource>` map it would POST to /api/v2/graphs/plan and
 * putting it in the URL *fragment* (the part after '#'), e.g.
 *
 *     /builder#graph=<base64url(gzip(JSON))>
 *
 * The fragment is never sent to the server, proxies, or logs and is not
 * included in the Referer header, so the graph stays client-side only. The
 * Builder decodes it into a *new* workspace tab by reusing the same node-
 * injection path recipes use (see nodesFromResourceMap / addNodesToWorkspace),
 * so it never clobbers the user's existing tabs, and the user still drives
 * Plan -> Execute themselves.
 *
 * "Create-only" here is a *convention* for the trusted agent->user handoff, not
 * a security guarantee, and isCreateOnlyResource is a sanity guard, not
 * enforcement. Two reasons: the backend classifies each resource as a create or
 * an update by database existence of its id (GraphEngine.planGraphUpdate), not
 * by the tmp prefix; and a browser tab is not a security boundary — Execute runs
 * against the same authenticated session regardless. The checks below just keep
 * a trusted handoff aligned with how the Builder mints new nodes and stop a
 * malformed payload from reaching the canvas. Enforcing create-only for
 * untrusted or shareable links would require server-side validation.
 */
export type ResourceMap = Record<string, INodeData>;

const GRAPH_FRAGMENT_RE = /[#&]graph=([^&]+)/;

// gzip streams start with the magic bytes 0x1f 0x8b.
const isGzip = (bytes: Uint8Array): boolean =>
    bytes.length > 2 && bytes[0] === 0x1f && bytes[1] === 0x8b;

// base64url string -> raw bytes
const base64urlToBytes = (value: string): Uint8Array => {
    const b64 = value.replace(/-/g, '+').replace(/_/g, '/');
    const binary = atob(b64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
};

// Accepts both gzipped and plain (uncompressed) payloads so links stay
// human-decodable in dev while real handoffs can compress. DecompressionStream
// is reached via globalThis so this compiles even on TS lib versions whose
// dom typings predate it; it is guarded for runtimes that lack the API.
const bytesToText = async (bytes: Uint8Array): Promise<string> => {
    const globalObj = globalThis as any;
    if (isGzip(bytes) && typeof globalObj.DecompressionStream !== 'undefined') {
        const stream = new Response(bytes).body!.pipeThrough(new globalObj.DecompressionStream('gzip'));
        return new Response(stream as any).text();
    }
    return new TextDecoder().decode(bytes);
};

/**
 * Validate a handed-off resource. This does two jobs at once:
 *  - Correctness: guarantees decodeGraphFragment never returns something that
 *    throws later in nodesFromResourceMap — value must be a plain object keyed
 *    by its own id, with the fields we dereference (resourceDefinitionClass to
 *    render, desiredState to Plan).
 *  - Create-only *convention* (a sanity guard, not enforcement — see the file
 *    header): a temporary id (NEW_NODE_ID_PREFIX, matching how the Builder mints
 *    new nodes) and no deletion.
 * `deleted` must be absent or literally false; anything else is rejected,
 * including the string "true", which Gson coerces to boolean server-side.
 * A failing resource rejects the whole payload (decodeGraphFragment -> null).
 */
const isCreateOnlyResource = (id: string, value: unknown): value is INodeData => {
    if (!value || typeof value !== 'object') {
        return false;
    }
    const r = value as Record<string, unknown>;
    // create-only convention: temporary id that matches its map key.
    if (typeof r.id !== 'string' || r.id !== id || !r.id.startsWith(NEW_NODE_ID_PREFIX)) {
        return false;
    }
    // no deletion. Accept only absent or literal false — reject e.g. "true"
    // (string), which would survive Gson's boolean coercion on the server.
    if ('deleted' in r && r.deleted !== false) {
        return false;
    }
    // shape required to render a node (type) and to Plan (desiredState).
    if (typeof r.resourceDefinitionClass !== 'string') {
        return false;
    }
    if (!r.desiredState || typeof r.desiredState !== 'object') {
        return false;
    }
    return true;
};

/**
 * Whether a `#graph=` payload is present at all. Lets callers distinguish
 * "no handoff link" from "a handoff link that failed validation" — both of
 * which decodeGraphFragment reports as null — so a rejected payload can still
 * be scrubbed from the URL.
 */
export const hasGraphFragment = (hash: string): boolean => GRAPH_FRAGMENT_RE.test(hash);

/**
 * Decode a `#graph=<encoded>` fragment into a resource map. Returns null when
 * the fragment is absent, empty, malformed, or not a valid create-only graph
 * (never throws).
 */
export const decodeGraphFragment = async (hash: string): Promise<ResourceMap | null> => {
    const match = GRAPH_FRAGMENT_RE.exec(hash);
    if (!match) {
        return null;
    }
    try {
        const bytes = base64urlToBytes(match[1]);
        const json = await bytesToText(bytes);
        const parsed = JSON.parse(json);
        if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
            return null;
        }
        const entries = Object.entries(parsed);
        if (entries.length === 0) {
            return null;
        }
        // Reject the whole payload unless every resource is create-only and
        // well-formed; a partial hydrate of a tampered graph is worse than none.
        if (!entries.every(([id, value]) => isCreateOnlyResource(id, value))) {
            return null;
        }
        return parsed as ResourceMap;
    } catch (error) {
        console.error('Failed to decode graph handoff fragment', error);
        return null;
    }
};

/**
 * Fill each resource's `desiredState` with its type's JSON-schema defaults,
 * mirroring what the Builder form (@rjsf) injects the moment a node is opened /
 * "Update Plan" is clicked (NewNodePrompt). A handoff-injected node otherwise
 * skips the form entirely, so schema-defaulted-but-absent fields (e.g. a
 * PubSubTopic's `customConfigs`) are missing — and some resource Plans
 * dereference those fields without a null check, turning an incomplete graph
 * into a backend 500. Applying defaults here makes a handoff node byte-for-byte
 * equivalent to a form-built one for every resource type, not just one field.
 *
 * getDefaultFormState only fills gaps, so any value already supplied in the
 * handoff payload is preserved. Resources whose type is unknown to the current
 * definitions are passed through untouched.
 */
export const applyResourceDefaults = (
    map: ResourceMap,
    resourceDefinitions: IResourceDefinitions | null
): ResourceMap => {
    const defs = resourceDefinitions?.resourceMap ?? {};
    const out: ResourceMap = {};
    for (const [id, resource] of Object.entries(map)) {
        const cls = resource.resourceDefinitionClass;
        const schema = cls ? defs[cls]?.configSchema : undefined;
        if (schema) {
            out[id] = {
                ...resource,
                desiredState: utils.getDefaultFormState(schema, resource.desiredState ?? {}, schema) as Record<
                    string,
                    unknown
                >,
            };
        } else {
            out[id] = resource;
        }
    }
    return out;
};

/**
 * Turn a resource map into React Flow nodes. Mirrors the recipe load transform
 * (RecipeModal.handleAddToWorkspace) so both share one code path.
 */
export const nodesFromResourceMap = (map: ResourceMap): TNode[] =>
    Object.values(map).map((resource) => ({
        position: { x: 0, y: 0 }, // will auto layout
        type: resource.resourceDefinitionClass,
        data: resource,
        id: resource.id,
    }));
