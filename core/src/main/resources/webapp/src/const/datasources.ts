import { INodeData } from './types';

export const fetchResource = async (
    resourceId: string,
    successCallback?: (resp: any) => any,
    errorCallback?: (err: any) => any
): Promise<null | INodeData> => {
    try {
        const response = await fetch('/api/v2/resources/' + resourceId);
        if (!response.ok) {
            console.error(response);
            const message = `Bad response: ${response.status}`;
            throw new Error(message);
        }
        const resourceJson: INodeData = await response.json();
        successCallback && successCallback(resourceJson);
        return resourceJson;
    } catch (error) {
        console.error(`Error fetching resource: ${resourceId}`, error);
        errorCallback && errorCallback(error);
    }
    return null;
};
