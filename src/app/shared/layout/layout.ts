import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../navbar/navbar';
import { FooterComponent } from '../footer/footer';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, NavbarComponent, FooterComponent],
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LayoutComponent {}
