import React from 'react';
import { LineChart, Line, Tooltip, XAxis, YAxis } from 'recharts';
import moment from 'moment';
import Typography from '@material-ui/core/Typography';

export default class StatsboardChart extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      dataLoaded: false,
      response: null,
    };
  }

  async fetchData(query) {
    const url = "/api/v2/metrics?query="+query
    let response = await fetch(url);
    let data = await response.json();
    return data;
  }

  componentDidMount = () => {
    this.fetchData(this.props.query).then(data => {
      this.setState({
        dataLoaded: true,
        response: data
      });
    });
  }

  componentDidUpdate(prevProps) {
    if (prevProps.query !== this.props.query) {
      this.fetchData(this.props.query).then(data => {
        this.setState({
          dataLoaded: true,
          response: data
        });
      });
    }
  }

  tickFormatter(timestamp) {
    return moment(timestamp*1000).format('HH:mm');
  }

  formatLabel(label) {
    return moment(label*1000).format('MMMM Do YYYY, h:mm:ss a');
  }

  formatResponse(response) {
    var seriesSet = new Set();
    var tempDatapointsObj = {};
    for (var responseGroup of response) {
      var datapointsForGroup = responseGroup.datapoints;
      if (datapointsForGroup.length === 0) {
        continue;
      }
      var tags = Object.values(responseGroup['tags']);
      var metric = responseGroup.metric;
      var seriesName = 'y';
      if (tags.length > 0) {
        seriesName = tags[0];
      }

      seriesSet.add(seriesName);
      for (var datapoint of datapointsForGroup) {
        var x = datapoint[0];
        var y = datapoint[1];
        if (!(x in tempDatapointsObj)) {
          tempDatapointsObj[x] = {};
        }
        tempDatapointsObj[x]['x'] = x;
        tempDatapointsObj[x][seriesName] = y;
      }
    }
    return [Object.values(tempDatapointsObj), seriesSet];
  }

  mapDataSeries(seriesSet, maxSeriesNum) {
    const colors = ['#8884d8', '#560694', '#018E2B', '#0C038B', '#850000'];
    var mappedLines = [];
    var i = 0;
    for (var seriesName of Array.from(seriesSet)) {
      if (i >= maxSeriesNum) {
        break;
      }
      mappedLines.push(
        <Line type='monotone' dataKey={seriesName} stroke={colors[i % colors.length]} dot={false} key={seriesName}/>
      );
      i += 1;
    }
    return mappedLines;
  }

  render() {
    if (this.state.response !== null && this.state.dataLoaded) {
      let [ formattedResponse, seriesSet ] = this.formatResponse(this.state.response);
      let lines = this.mapDataSeries(seriesSet, 8);
      if (lines.length === 0) {
        return (
          <Typography variant="overline" display="block" gutterBottom >
            No data available
          </Typography>
        );
      }
      return (
        <LineChart
          width={330}
          height={230}
          data={formattedResponse}
          margin={{left: 2, right: 10}}
        >
          <XAxis
            dataKey = 'x'
            tickFormatter={this.tickFormatter}
            tick={{fontSize: 10}}
            interval={30}
          />
          <YAxis tick={{fontSize: 10}} />
          <Tooltip
            labelFormatter={this.formatLabel}
            wrapperStyle={{maxHeight: 100, opacity: 0.8, fontSize: 10}}
          />
          {lines}
        </LineChart>
      );
    }
    return (
      <div />
    )
  }
}