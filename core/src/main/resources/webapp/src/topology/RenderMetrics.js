import { Typography } from '@material-ui/core';
import React from 'react';
import StatsboardChart from '../components/metrics/StatsboardChart';
import { Box } from '@mui/material';

class RenderMetrics extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            metrics: null,
            resource: props.resource,
        };
    }

    componentDidMount() {
        let resource = this.state.resource;
        fetch('/api/v2/resources/metrics/' + resource.id)
            .then((response) => {
                if (response.ok) {
                    return response.json();
                } else {
                    throw new Error('Resource fetch failed for:' + resource.id);
                }
            })
            .then((data) => {
                this.setState({
                    metrics: data,
                });
            })
            .catch((error) => {
                console.log(error);
            });
    }

    renderMetrics() {
        return (
            <Box
                sx={{
                    display: 'flex',
                    flexWrap: 'wrap',
                }}
            >
                {this.state.metrics.map((t) => (
                    <Box
                        key={t.metricLabel}
                        sx={{
                            width: '50%',
                            minHeight: '250px',
                        }}
                    >
                        <Box sx={{ marginY: 1 }}>
                            <Typography>{t.metricLabel}</Typography>
                        </Box>
                        <StatsboardChart query={t.query} />
                    </Box>
                ))}
            </Box>
        );
    }

    render() {
        if (this.state.metrics && this.state.metrics.length > 0) {
            return this.renderMetrics();
        } else {
            return (
                <Typography>
                    No metrics available for this Resource, please make sure the Resource has been provisioned. If it's
                    provisioned and this is empty then likely no metrics are provided by the Author.
                </Typography>
            );
        }
    }
}

export default RenderMetrics;
