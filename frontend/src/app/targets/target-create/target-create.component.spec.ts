import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TargetCreateComponent } from './target-create.component';

describe('TargetCreateComponent', () => {
  let component: TargetCreateComponent;
  let fixture: ComponentFixture<TargetCreateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TargetCreateComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TargetCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
