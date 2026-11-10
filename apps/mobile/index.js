import { registerRootComponent } from 'expo';
import App from './App';

// registerRootComponent calls AppRegistry.registerComponent('main', () => App);
// It ensures that whether the app is loaded in Expo or standalone native build,
// the root component is registered properly.
registerRootComponent(App);
