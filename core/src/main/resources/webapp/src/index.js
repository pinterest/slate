import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import { BrowserRouter as Router } from 'react-router-dom';
import { SnackbarProvider } from './context/SnackbarContext';
import { LoadingSpinnerProvider } from './context/LoadingSpinnerContext';
import store from './store';
import App from './App';
import * as serviceWorker from './serviceWorker';
import TutorialProvider from './context/TutorialProvider';

ReactDOM.render(
    <Provider store={store}>
        <Router>
            <SnackbarProvider>
                <LoadingSpinnerProvider>
                    <TutorialProvider>
                        <App />
                    </TutorialProvider>
                </LoadingSpinnerProvider>
            </SnackbarProvider>
        </Router>
    </Provider>,
    document.getElementById('root')
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
