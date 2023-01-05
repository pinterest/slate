export const config = {
  apiVersion: "v1",
  graphStylesheet: [
    {
      selector: 'node[ntype="child"]',
      style: {
        label: "data(label)",
        height: 30,
        width: "label",
        "text-halign": "center",
        "text-valign": "center",
        shape: "round-rectangle",
        "border-width": 1,
        "border-color": "#4a4a4a",
      },
    },
    {
      selector: 'node[type="ps_topic_"]',
      style: {
        "background-color": "#77aad9",
      },
    },
    {
      selector: "edge",
      style: {
        "target-arrow-shape": "triangle",
        "curve-style": "taxi",
        "source-endpoint": "outside-to-node",
        "target-endpoint": "outside-to-node",
        "taxi-direction": "rightward",
        width: "2px",
        "line-color": "#000000",
        "target-arrow-color": "#000000",
      },
    },
    {
      selector: 'node[ntype="parent"]',
      style: {
        label: "data(label)",
        "text-valign": "top",
        shape: "round-rectangle",
      },
    },
  ],
  graphStyle: {
    width: "100vw",
    height: "80vh",
  },
  statsboardGraphUrl: "https://statsboard.pinadmin.com/build2?settings=",
  nodeColors: {
    green: "#8bc34a",
    yellow: "#ffeb3b",
    red: "#f44336",
  },
};
