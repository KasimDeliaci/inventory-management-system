import { Routes } from '@angular/router';
import { ProductPageComponent } from './pages/product-page/product-page';
import { SupplierPageComponent } from './pages/supplier-page/supplier-page';
import { CustomerPageComponent } from './pages/customer-page/customer-page';
import { OrderPageComponent } from './pages/order-page/order-page';

export const routes: Routes = [
  { path: '', redirectTo: 'products', pathMatch: 'full' },
  { path: 'products', component: ProductPageComponent },
  { path: 'suppliers', component: SupplierPageComponent },
  { path: 'customers', component: CustomerPageComponent },
  { path: 'orders', component: OrderPageComponent },
  { path: '**', redirectTo: 'products' } // This must be last
];