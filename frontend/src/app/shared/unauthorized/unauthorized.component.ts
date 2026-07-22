import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../navbar/navbar.component';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [RouterLink, NavbarComponent],
  templateUrl: './unauthorized.component.html'
})
export class UnauthorizedComponent {}