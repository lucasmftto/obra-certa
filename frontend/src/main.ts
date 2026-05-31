import { registerLocaleData } from '@angular/common';
import localePtBr from '@angular/common/locales/pt';
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

registerLocaleData(localePtBr);

bootstrapApplication(AppComponent, appConfig).catch(err => console.error(err));
