import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) {
        const mensagem = error.error?.mensagem ?? 'Ocorreu um erro inesperado.';
        snackBar.open(mensagem, 'Fechar', { duration: 5000, panelClass: 'snack-error' });
      }
      return throwError(() => error);
    }),
  );
};
