import React from 'react';
import FiberManualRecordIcon from '@material-ui/icons/FiberManualRecord';
import Typography from '@material-ui/core/Typography';
import Paper from '@material-ui/core/Paper';
import { config } from './Config.js';

export default class ColorLegend extends React.Component {
  constructor(props) {
    super(props);
  }

  getColorLegend() {
    let colorMetrics = this.props.selectedNode.data().colorMetrics;
    if (colorMetrics) {
      let inequality0 = ' <';
      let inequality1 = ' >';
      let threshold0 = colorMetrics.colorThreshold0;
      let threshold1 = colorMetrics.colorThreshold1;
      if (colorMetrics.colorWorstCase === 'min') {
        inequality0 = ' >';
        inequality1 = ' <';
        threshold0 = colorMetrics.colorThreshold1;
        threshold1 = colorMetrics.colorThreshold0;
      }
      return (
        <div className={this.props.classes.sidebarContentContainer}>
          <Typography style={{overflowWrap: "break-word"}} >
            <Typography variant="button" display="block" gutterBottom >
              {'color code'}
            </Typography>
            <Typography variant="caption" display="block" gutterBottom >
              {colorMetrics.colorAPI}
            </Typography>
            <Typography display='block' >
              <FiberManualRecordIcon
                style={{height: 8, width: 8, fill: config.nodeColors.green}}
                display='inline'
              />
              <Typography variant="caption" display='inline' gutterBottom >
                {inequality0 + threshold0}
              </Typography>
            </Typography>
            <Typography display='block' >
              <FiberManualRecordIcon
                style={{height: 8, width: 8, fill: config.nodeColors.red}}
                display='inline'
              />
              <Typography variant="caption" display='inline' gutterBottom >
                {inequality1 + threshold1}
              </Typography>
            </Typography>
            <Typography display='block' >
              <FiberManualRecordIcon
                style={{height: 8, width: 8, fill: '#9e9e9e'}}
                display='inline'
              />
              <Typography variant="caption" display='inline' gutterBottom >
                {' No Data'}
              </Typography>
            </Typography>
          </Typography>
        </div>
      );
    } else {
      return (
        <div className={this.props.classes.sidebarContentContainer}>
          <Typography variant="button" display="block" gutterBottom >
            {'color code'}
          </Typography>
          <Typography variant="caption" display='block' gutterBottom >
            No color metrics specified for node.
          </Typography>
        </div>
      );
    }
  }

  render() {
    if (this.props.selectedNode && !this.props.isUnselectEvent) {
      return (
        <Paper className={this.props.classes.colorLegend}>
          {this.getColorLegend()}
        </Paper>
      );
    } else {
      return <div></div>;
    }
  }
}
