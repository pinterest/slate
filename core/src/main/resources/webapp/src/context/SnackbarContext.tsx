import React, { useState, createContext, useContext } from 'react';
import { SnackbarProps, Snackbar, Slide, AlertColor, Alert, SnackbarCloseReason } from '@mui/material';

interface ISnackbarProps {
    type: 'simple' | AlertColor;
    message: null | string;
    props?: Omit<SnackbarProps, 'message'>;
}
interface ISnackbarContext {
    showSnackbar: (obj: ISnackbarProps) => void;
    snackbar: ISnackbarProps;
}

const defaultSnackbar: ISnackbarProps = { type: 'simple', message: null };
export const SnackbarContext = createContext<ISnackbarContext>({
    showSnackbar: () => {},
    snackbar: defaultSnackbar,
});

interface ISnackbarProviderProps {}
export const SnackbarProvider: React.FC<ISnackbarProviderProps> = ({ children }) => {
    const [snackbar, setSnackbar] = useState<ISnackbarProps>(defaultSnackbar);

    const handleShowSnackbar = (obj: ISnackbarProps) => {
        setSnackbar(obj);
    };
    const contextValue: ISnackbarContext = {
        showSnackbar: handleShowSnackbar,
        snackbar,
    };
    const { type, message } = snackbar;

    // set default props first and they can be overridden by consumer passed obj
    const snackBarProps: SnackbarProps = {
        TransitionComponent: (props: any) => <Slide {...props} direction="up" />,
        open: !!message,
        anchorOrigin: {
            vertical: 'bottom',
            horizontal: 'right',
        },
        autoHideDuration: snackbar.props?.autoHideDuration ?? 3000,
        style: {
            maxWidth: '60%',
            maxHeight: '100px',
            overflow: 'scroll',
        },
        ...snackbar.props,
        onClose: (event: React.SyntheticEvent<any> | Event, reason: SnackbarCloseReason) => {
            // first execute user passed close handler if any
            if (snackbar.props && snackbar.props.onClose) {
                snackbar.props.onClose(event, reason);
            }
            // close this snackbar, otherwise it will show everytime
            setSnackbar(defaultSnackbar);
        },
    };
    return (
        <SnackbarContext.Provider value={contextValue}>
            {children}
            {type == 'simple' ? (
                <Snackbar {...snackBarProps} message={message} />
            ) : (
                <Snackbar {...snackBarProps}>
                    <Alert severity={type} variant="filled">
                        {message}
                    </Alert>
                </Snackbar>
            )}
        </SnackbarContext.Provider>
    );
};

export const useSnackBar = () => useContext(SnackbarContext);
