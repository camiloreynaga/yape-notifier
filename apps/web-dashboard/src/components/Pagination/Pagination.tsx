/**
 * Componente de paginación mejorado con números de página
 * y página actual destacada
 */

import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';
import clsx from 'clsx';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  from?: number;
  to?: number;
  total?: number;
  searchActive?: boolean;
  filteredCount?: number;
  className?: string;
}

export default function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  from,
  to,
  total,
  searchActive = false,
  filteredCount,
  className,
}: PaginationProps) {
  // Generate page numbers to display
  const getPageNumbers = () => {
    const pages: (number | 'ellipsis')[] = [];
    const maxVisible = 5;

    if (totalPages <= maxVisible + 2) {
      // Show all pages if total is small
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      // Always show first page
      pages.push(1);

      // Calculate range around current page
      let start = Math.max(2, currentPage - 1);
      let end = Math.min(totalPages - 1, currentPage + 1);

      // Adjust if at the beginning
      if (currentPage <= 3) {
        end = Math.min(totalPages - 1, maxVisible - 1);
      }

      // Adjust if at the end
      if (currentPage >= totalPages - 2) {
        start = Math.max(2, totalPages - maxVisible + 2);
      }

      // Add ellipsis before if needed
      if (start > 2) {
        pages.push('ellipsis');
      }

      // Add middle pages
      for (let i = start; i <= end; i++) {
        pages.push(i);
      }

      // Add ellipsis after if needed
      if (end < totalPages - 1) {
        pages.push('ellipsis');
      }

      // Always show last page
      pages.push(totalPages);
    }

    return pages;
  };

  const pageNumbers = getPageNumbers();

  return (
    <nav
      className={clsx(
        'flex flex-col sm:flex-row items-center justify-between gap-4 p-4 sm:p-6 bg-white rounded-xl shadow-sm border border-gray-100',
        className
      )}
      role="navigation"
      aria-label="Paginación de notificaciones"
    >
      {/* Info de resultados */}
      <div className="text-sm text-gray-700">
        {searchActive && filteredCount !== undefined ? (
          <>
            Mostrando <span className="font-semibold text-primary-600">{filteredCount}</span> de{' '}
            <span className="font-semibold">{total}</span> resultados
            <span className="inline-flex items-center ml-2 px-2 py-0.5 rounded-full text-xs font-medium bg-primary-100 text-primary-700">
              Búsqueda activa
            </span>
          </>
        ) : from && to && total ? (
          <>
            Mostrando <span className="font-semibold">{from}</span> a{' '}
            <span className="font-semibold">{to}</span> de{' '}
            <span className="font-semibold">{total}</span> resultados
          </>
        ) : null}
      </div>

      {/* Controles de paginación */}
      <div className="flex items-center gap-1">
        {/* Ir a primera página */}
        <button
          onClick={() => onPageChange(1)}
          disabled={currentPage === 1}
          className="p-2 rounded-lg text-gray-500 hover:text-primary-600 hover:bg-primary-50 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-gray-500 transition-all"
          aria-label="Ir a primera página"
          title="Primera página"
        >
          <ChevronsLeft className="h-4 w-4" aria-hidden="true" />
        </button>

        {/* Página anterior */}
        <button
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 1}
          className="p-2 rounded-lg text-gray-500 hover:text-primary-600 hover:bg-primary-50 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-gray-500 transition-all"
          aria-label="Página anterior"
          title="Página anterior"
        >
          <ChevronLeft className="h-4 w-4" aria-hidden="true" />
        </button>

        {/* Números de página */}
        <div className="flex items-center gap-1 mx-1">
          {pageNumbers.map((page, index) =>
            page === 'ellipsis' ? (
              <span
                key={`ellipsis-${index}`}
                className="px-2 py-1 text-gray-400"
                aria-hidden="true"
              >
                ...
              </span>
            ) : (
              <button
                key={page}
                onClick={() => onPageChange(page)}
                disabled={page === currentPage}
                className={clsx(
                  'min-w-[36px] h-9 px-3 rounded-lg text-sm font-medium transition-all',
                  page === currentPage
                    ? 'bg-primary-600 text-white shadow-md shadow-primary-600/30 cursor-default'
                    : 'text-gray-700 hover:bg-gray-100 hover:text-primary-600'
                )}
                aria-label={`Ir a página ${page}`}
                aria-current={page === currentPage ? 'page' : undefined}
              >
                {page}
              </button>
            )
          )}
        </div>

        {/* Página siguiente */}
        <button
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage === totalPages}
          className="p-2 rounded-lg text-gray-500 hover:text-primary-600 hover:bg-primary-50 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-gray-500 transition-all"
          aria-label="Página siguiente"
          title="Página siguiente"
        >
          <ChevronRight className="h-4 w-4" aria-hidden="true" />
        </button>

        {/* Ir a última página */}
        <button
          onClick={() => onPageChange(totalPages)}
          disabled={currentPage === totalPages}
          className="p-2 rounded-lg text-gray-500 hover:text-primary-600 hover:bg-primary-50 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-gray-500 transition-all"
          aria-label="Ir a última página"
          title="Última página"
        >
          <ChevronsRight className="h-4 w-4" aria-hidden="true" />
        </button>
      </div>
    </nav>
  );
}
