import * as React from "react";
import ReactFlow, {
  ReactFlowProvider,
  removeElements,
  updateEdge,
  addEdge,
  MiniMap,
  Controls,
  Background,
  Handle,
  isNode,
  isEdge,
} from "react-flow-renderer";
import dagre from "dagre";
import { TabContext, TabList, TabPanel } from "@material-ui/lab";
import { Box, Button, Tab, TextField, Typography } from "@material-ui/core";
import JSONPretty from "react-json-pretty";
import "react-json-pretty/themes/monikai.css";

const nodeWidth = 250;
const nodeHeight = 36;

const nodeTypes = {
  taskNode: TaskNode,
};

function getRandomIntInclusive(min, max) {
  min = Math.ceil(min);
  max = Math.floor(max);
  return Math.floor(Math.random() * (max - min + 1) + min); //The maximum is inclusive and the minimum is inclusive
}

function getWindowDimensions() {
  const { innerWidth: width, innerHeight: height } = window;
  return {
    width,
    height,
  };
}

export default function ProcessDesigner(props) {
  const [windowDimensions, setWindowDimensions] = React.useState(
    getWindowDimensions()
  );
  const [newTaskId, setNewTaskId] = React.useState("");
  const [newTaskDefinition, setNewTaskDefinition] = React.useState("");

  const handleNewTaskId = (event) => {
    setNewTaskId(event.target.value);
  };
  const handleNewTaskDefinition = (event) => {
    setNewTaskDefinition(event.target.value);
  };

  const [elements, setElements] = React.useState([]);
  const onElementsRemove = (elementsToRemove) => {
    setElements((els) => removeElements(elementsToRemove, els));
  };
  const removeElementById = (id) => {
    setElements((els) => {
      let elementsToRemove = els.find((obj) => obj.id === id);
      return removeElements([elementsToRemove], els);
    });
  };
  const onEdgeUpdate = (oldEdge, newConnection) =>
    setElements((els) => updateEdge(oldEdge, newConnection, els));
  const onConnect = (params) => {
    setElements((els) =>
      addEdge(
        {
          ...params,
          type: "smoothstep",
          animated: true,
          arrowHeadType: "arrowclosed",
          data: {
            removeElementById: removeElementById,
          },
        },
        els
      )
    );
  };
  function handleNodeUpdate(nodeData) {
    let existing = elements.findIndex((e) => e.id === nodeData.id);
    if (existing >= 0) {
      elements[existing] = nodeData;
      setElements(elements);
    } else {
      setElements((es) => es.concat(nodeData));
    }
    // setSelectedNode(null);
  }
  return (
    <ReactFlowProvider>
      <div
        id="resourceCanvas"
        style={{
          width: windowDimensions.width - 50,
          marignTop: "45px",
          float: "left",
          height: windowDimensions.height - 200,
        }}
      >
        <TextField
          label="New Task Id"
          value={newTaskId}
          onChange={handleNewTaskId}
        />
        <TextField
          label="New Task Definition"
          value={newTaskDefinition}
          onChange={handleNewTaskDefinition}
        />
        <Button
          onClick={() => {
            const position = {
              x: getRandomIntInclusive(100, 500),
              y: getRandomIntInclusive(100, 500),
            };
            let n = {
              id: newTaskId,
              type: "taskNode",
              position,
              data: { label: newTaskDefinition },
            };
            console.log(n);
            setNewTaskId("");
            setNewTaskDefinition("");
            handleNodeUpdate(n);
          }}
          variant="contained"
          color="primary"
        >
          Add node
        </Button>
        <Button
          onClick={() => {
            console.log(elements);
          }}
          variant="contained"
          color="primary"
        >
          Export
        </Button>
        <ReactFlow
          elements={elements}
          onElementsRemove={onElementsRemove}
          onEdgeUpdate={onEdgeUpdate}
          onConnect={onConnect}
          onNodeDoubleClick={(event, node) => {
            // setSelectedNode(node);
            // setEditResourceDrawer(true);
          }}
          nodeTypes={nodeTypes}
          snapGrid={[15, 15]}
          deleteKeyCode={8}
          minZoom={0.1}
          maxZoom={6}
          defaultZoom={1.2}
        >
          <MiniMap />
          <Controls />
          <Background />
        </ReactFlow>
      </div>
    </ReactFlowProvider>
  );
}

function TaskNode(node) {
  let fontSize = "2pt";
  return (
    <div className="react-flow__node-default" style={{ color: "black" }}>
      <Handle
        id="target"
        type="target"
        position="left"
        style={{ background: "#555" }}
      >
        <Typography
          style={{
            fontSize: fontSize,
            left: "8px",
            position: "relative",
          }}
        >
          Trigger
        </Typography>
      </Handle>
      <div>
        <div style={{ fontSize: "0.8vw" }}>{node.data.label}</div>
        <div style={{ fontSize: "5pt" }}>{node.id}</div>
      </div>
      <Handle
        type="source"
        position="right"
        id="succeeded"
        style={{ background: "green" }}
      >
        <Typography
          style={{
            fontSize: fontSize,
            float: "right",
            left: "-8px",
            position: "relative",
          }}
        >
          Succeeded
        </Typography>
      </Handle>
      <Handle
        type="source"
        position="bottom"
        id="failed"
        style={{ left: 40, background: "orange" }}
      >
        <Typography
          style={{
            fontSize: fontSize,
            float: "right",
            botton: "8px",
            position: "relative",
          }}
        >
          Failed
        </Typography>
      </Handle>
      <Handle
        type="source"
        position="bottom"
        id="cancelled"
        style={{ left: 110, background: "red" }}
      >
        <Typography
          style={{
            fontSize: fontSize,
            float: "right",
            botton: "8px",
            position: "relative",
          }}
        >
          Cancelled
        </Typography>
      </Handle>
    </div>
  );
}
