import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-side-nav',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './side-nav.html',
  styleUrls: ['./side-nav.scss']
})
export class SideNav {}
