import { Routes } from '@angular/router';
import { ProductPageComponent } from './pages/product-page/product-page';

export const routes: Routes = [
  { path: '', redirectTo: 'products', pathMatch: 'full' },
  { path: 'products', component: ProductPageComponent },
  { path: '**', redirectTo: 'products' }
];
