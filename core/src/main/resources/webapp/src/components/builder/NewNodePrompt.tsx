import React, { useMemo } from 'react';
import { Button, Tooltip, Typography } from '@material-ui/core';
import Form from '@rjsf/material-ui';
import { cloneDeep } from 'lodash';
import { TNode, INodeData, IResourceMap } from '../../const/types';

interface INewNodePromptProps {
    handleNodeUpdate: (node: TNode) => void;
    node: TNode;
    resourceDefinition: IResourceMap;
}

const NewNodePrompt: React.FC<INewNodePromptProps> = ({ handleNodeUpdate, node, resourceDefinition }) => {
    // data & type always exists on node
    const nodeData = node.data as INodeData;
    const nodeType = node.type as string;

    const resource = useMemo(() => {
        const newData = cloneDeep(nodeData);
        if (nodeData && nodeData.owner) {
            newData.desiredState.owner = nodeData.owner;
            newData.desiredState.project = nodeData.project;
            newData.desiredState.region = nodeData.region;
            newData.desiredState.environment = nodeData.environment;
        }
        return newData;
    }, [node]);

    const onSubmitHandler: any = ({ formData }: any, e: any) => {
        console.log('Submitted:' + JSON.stringify(formData));
        resource.owner = formData.owner;
        resource.project = formData.project;
        resource.region = formData.region;
        resource.environment = formData.environment;
        resource.desiredState = formData;
        const newNode = {
            ...node,
            data: resource,
        };
        handleNodeUpdate(newNode);
    };

    let formSchema = resourceDefinition[nodeType].configSchema;
    let uiSchema = resourceDefinition[nodeType].uiSchema ? resourceDefinition[nodeType].uiSchema : {};
    return (
        <Form
            schema={formSchema}
            uiSchema={uiSchema}
            formData={resource.desiredState}
            onSubmit={onSubmitHandler}
            showErrorList={false}
            liveValidate
        >
            <Tooltip title="Update the Resource in your Plan. Note: nothing will execute when you save.">
                <Button type="submit" variant="contained" color="primary">
                    Update Plan
                </Button>
            </Tooltip>
        </Form>
    );
};

export default NewNodePrompt;
