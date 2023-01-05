import { makeStyles } from '@material-ui/core/styles';
const drawerWidth = 150;
export const useStyles = makeStyles((theme) => ({
    root: {
        display: 'flex',
        margin: 0,
    },
    appBar: {
        marginLeft: 0,
        zIndex: theme.zIndex.drawer + 1,
        backgroundColor: '#2b5185',
    },
    toolbar: theme.mixins.toolbar,
    content: {
        flexGrow: 1,
        padding: theme.spacing(3),
        backgroundColor: theme.palette.background.default,
    },
    title: {
        marginTop: theme.spacing(5),
    },
    formControl: {
        margin: theme.spacing(3),
        width: 200,
    },
    button: {
        marginLeft: theme.spacing(1),
        marginTop: theme.spacing(1),
    },
    exportButton: {
        marginLeft: theme.spacing(1),
    },
    deckDropdownButton: {
        marginBottom: theme.spacing(1),
    },
    rightIcon: {
        marginLeft: theme.spacing(1),
    },
    container: {
        display: 'flex',
        flexWrap: 'wrap',
        flexDirection: 'row',
    },
    input: {
        margin: theme.spacing(1),
        borderColor: theme.palette.common.white,
    },
    card: {
        margin: 0,
        top: 65,
        right: 'auto',
        bottom: 'auto',
        left: 20,
        position: 'fixed',
        'z-index': 1,
    },
    filterIcon: {
        position: 'absolute',
        top: 60,
        left: 10,
        'z-index': 1,
    },
    paper: {
        position: 'absolute',
        width: 400,
        backgroundColor: theme.palette.background.paper,
        border: '2px solid #000',
        boxShadow: theme.shadows[5],
        padding: theme.spacing(2, 4, 4),
        outline: 'none',
    },
    searchBar: {
        position: 'fixed',
        right: 30,
    },
    drawer: {
        width: drawerWidth,
        flexShrink: 0,
        zIndex: 2,
    },
    drawerHeader: {
        alignItems: 'center',
        marginTop: 60,
        marginLeft: 10,
    },
    sidebarContentContainer: {
        marginTop: 10,
        marginLeft: 10,
        marginRight: 10,
    },
    colorLegend: {
        position: 'absolute',
        bottom: '5vh',
        left: 10,
        zIndex: 2,
        maxWidth: 300,
    },
    topologyForm: {
        margin: theme.spacing(3),
        width: 800,
    },
    drawerPaper: {
        width: drawerWidth,
    },
    drawerContainer: {
        overflow: 'auto',
    },
    table: {
        minWidth: 650,
    },
    builderPlanDialogPaper: {
        minWidth: '60vw',
        maxWidth: '60vw',
        minHeight: '55vh',
        maxHeight: '55vh',
        height: '100%',
    },
    executionDialogPaper: {
        minWidth: '95vw',
        maxWidth: '95vw',
        minHeight: '85vh',
        maxHeight: '85vh',
        height: '100%',
    },
    execTableRow: {
        cursor: 'pointer',
        '&:hover': {
            backgroundColor: '#efefef',
        },
        '& .MuiTableCell-root': {
            padding: '12px',
        },
    },
    tasksTable: {
        minWidth: 650,
    },
    tasksTableRow: {
        cursor: 'pointer',
        '& > *': {
            borderBottom: 'unset',
        },
        '&:hover': {
            backgroundColor: '#efefef',
        },
        '& .MuiTableCell-root': {
            padding: '12px',
        },
    },
    graphBorder: {
        border: '2px solid #dcdce3',
    },
}));
