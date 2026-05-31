import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ProjectService } from './projeto.service';

describe('ProjectService', () => {
  let service: ProjectService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ProjectService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(ProjectService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('list() deve fazer GET em /projects', () => {
    service.list().subscribe();
    const req = httpMock.expectOne(r => r.url.includes('/projects'));
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('create() deve fazer POST em /projects', () => {
    const dados = { name: 'Reforma', type: 'HOUSE' as const };
    service.create(dados).subscribe();
    const req = httpMock.expectOne(r => r.url.includes('/projects'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dados);
    req.flush({ id: 1, ...dados, status: 'IN_BUDGET' });
  });

  it('delete() deve fazer DELETE em /projects/:id', () => {
    service.delete(1).subscribe();
    const req = httpMock.expectOne(r => r.url.includes('/projects/1'));
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
