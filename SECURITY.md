# Slate Authentication and Authorization at Pinterest

At Pinterest, Slate runs behind a managed ingress and service platform. User-facing requests are authenticated by the platform before reaching Slate; the trusted identity context is then forwarded internally. Service-to-service calls use mutually authenticated transport and workload identities, so the receiving side can establish the calling service's identity without trusting caller-provided headers. Slate itself does not implement a browser sign-in flow or prescribe a specific identity provider.

Authorization is enforced in layers. Platform policy restricts which authenticated workloads can invoke Slate and may constrain access by API path or HTTP method; Slate then applies role-based authorization to protected operations using trusted user and group context. This separates service identity from end-user identity, supports defense in depth and auditing, and keeps identity providers, credential formats, policy rules, and role mappings deployment-specific rather than part of the open-source repository.


# Reporting Security Issues

If you discover a security issue in this project, please report it using
[Bugcrowd](https://bugcrowd.com/pinterest).

This will allow us to assess the risk and make a fix available before we
publish a public bug report.

Thanks for helping us make our software safe for everyone!
