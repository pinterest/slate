import React, { useState, createContext, useContext } from 'react';
import { Backdrop, CircularProgress } from '@mui/material';

interface ILoadingSpinnerContext {
    showLoadingOverlay: (val: boolean) => void;
    showOverlay: boolean;
}

export const LoadingSpinnerContext = createContext<ILoadingSpinnerContext>({
    showLoadingOverlay: () => {},
    showOverlay: false,
});

interface ILoadingSpinnerProviderProps {}
export const LoadingSpinnerProvider: React.FC<ILoadingSpinnerProviderProps> = ({ children }) => {
    const [showOverlay, setShowOverlay] = useState(false);

    const contextValue: ILoadingSpinnerContext = {
        showLoadingOverlay: (val: boolean) => {
            setShowOverlay(val);
        },
        showOverlay,
    };
    return (
        <LoadingSpinnerContext.Provider value={contextValue}>
            {children}
            <Backdrop sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 9999 }} open={showOverlay}>
                <CircularProgress color="inherit" />
            </Backdrop>
        </LoadingSpinnerContext.Provider>
    );
};

export const useLoadingSpinner = () => useContext(LoadingSpinnerContext);
