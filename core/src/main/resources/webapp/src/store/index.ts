import { configureStore, getDefaultMiddleware } from '@reduxjs/toolkit';
import builderSlice from './slice/builder';

let middleware;
const isDevelopment = process.env.NODE_ENV !== 'production';
if (isDevelopment) {
    // Escape hatch from immutable and serializable state checks while developing
    const [ImmutableStateInvariantMiddleware, ...customMiddleware] = getDefaultMiddleware();
    console.warn(
        `ImmutableStateInvariantMiddleware is being omitted for development mode. ${ImmutableStateInvariantMiddleware.name}`
    );
    middleware = customMiddleware;
} else {
    middleware = getDefaultMiddleware();
}

const reducer = {
    builder: builderSlice.reducer,
};

const store = configureStore({
    reducer,
    middleware,
});

export type RootState = ReturnType<typeof store.getState>;

export default store;
