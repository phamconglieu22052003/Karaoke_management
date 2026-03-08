(function () {
  const PAGE_SIZE_OPTIONS = [10, 20, 50];

  function getStorageKey(table) {
    const pageKey = table.dataset.pageKey || table.id || table.dataset.paginationFor || table.dataset.title || 'table';
    return `karaoke.pageSize.${pageKey}`;
  }

  function shouldPaginateTable(table) {
    if (!table) return false;
    if (table.dataset.paginate === 'false' || table.hasAttribute('data-no-paginate')) return false;
    if (table.id === 'linesTable') return false;
    return !!table.closest('.table-wrap') || table.hasAttribute('data-paginate');
  }

  function isPlaceholderRow(row, headerCount) {
    const cells = Array.from(row.children).filter((cell) => cell.tagName === 'TD' || cell.tagName === 'TH');
    if (!cells.length) return true;
    if (cells.length === 1) {
      const colspan = parseInt(cells[0].getAttribute('colspan') || '1', 10);
      if (colspan >= headerCount || row.hasAttribute('data-empty-row')) return true;
    }
    return row.hasAttribute('data-empty-row');
  }

  function ensureSttColumn(table) {
    if (table.dataset.sttReady === 'true') return;

    const theadRow = table.querySelector('thead tr');
    if (!theadRow) return;

    const sttTh = document.createElement('th');
    sttTh.textContent = 'STT';
    sttTh.className = 'stt-col';
    theadRow.insertBefore(sttTh, theadRow.firstElementChild);

    const headerCount = theadRow.children.length - 1;
    const bodyRows = Array.from(table.querySelectorAll('tbody tr'));
    bodyRows.forEach((row) => {
      const firstCell = row.firstElementChild;
      if (!firstCell) return;

      if (isPlaceholderRow(row, headerCount)) {
        const colspan = parseInt(firstCell.getAttribute('colspan') || String(headerCount), 10);
        firstCell.setAttribute('colspan', String(colspan + 1));
        const emptyTd = document.createElement('td');
        emptyTd.className = 'stt-cell';
        emptyTd.style.display = 'none';
        row.insertBefore(emptyTd, firstCell);
        return;
      }

      const sttTd = document.createElement('td');
      sttTd.className = 'stt-cell';
      sttTd.textContent = '';
      row.insertBefore(sttTd, row.firstElementChild);
    });

    table.dataset.sttReady = 'true';
  }

  function initTablePagination() {
    const tables = Array.from(document.querySelectorAll('table')).filter(shouldPaginateTable);

    tables.forEach((table, tableIndex) => {
      if (table.dataset.paginationInitialized === 'true') return;

      const tbody = table.querySelector('tbody');
      if (!tbody) return;

      ensureSttColumn(table);

      const rows = Array.from(tbody.querySelectorAll('tr'));
      const headerCount = table.querySelectorAll('thead tr th').length || 1;
      const dataRows = rows.filter((row) => !isPlaceholderRow(row, headerCount));
      const emptyRows = rows.filter((row) => isPlaceholderRow(row, headerCount));
      const defaultSize = parseInt(table.dataset.paginate || '10', 10);
      const savedSize = parseInt(localStorage.getItem(getStorageKey(table)) || '', 10);
      let pageSize = PAGE_SIZE_OPTIONS.includes(savedSize) ? savedSize : defaultSize;
      if (!PAGE_SIZE_OPTIONS.includes(pageSize)) pageSize = 10;
      let currentPage = 1;

      const existingWrap = table.parentElement && table.parentElement.nextElementSibling && table.parentElement.nextElementSibling.classList && table.parentElement.nextElementSibling.classList.contains('pagination-wrap')
        ? table.parentElement.nextElementSibling
        : null;
      if (existingWrap) existingWrap.remove();

      const wrap = document.createElement('div');
      wrap.className = 'pagination-wrap';
      wrap.dataset.paginationFor = table.id || `table-${tableIndex + 1}`;

      const left = document.createElement('div');
      left.className = 'pagination-meta';

      const info = document.createElement('div');
      info.className = 'pagination-info';

      const pageSizeWrap = document.createElement('label');
      pageSizeWrap.className = 'pagination-size';
      pageSizeWrap.innerHTML = '<span>Dữ liệu / trang</span>';

      const select = document.createElement('select');
      select.className = 'pagination-select';
      PAGE_SIZE_OPTIONS.forEach((size) => {
        const option = document.createElement('option');
        option.value = String(size);
        option.textContent = String(size);
        if (size === pageSize) option.selected = true;
        select.appendChild(option);
      });
      pageSizeWrap.appendChild(select);
      left.appendChild(info);
      left.appendChild(pageSizeWrap);

      const controls = document.createElement('div');
      controls.className = 'pagination-controls';

      function updateStt(startIndex) {
        dataRows.forEach((row, index) => {
          const sttCell = row.querySelector('.stt-cell');
          if (!sttCell) return;
          sttCell.textContent = String(startIndex + index + 1);
        });
      }

      function renderPage(page) {
        const totalRows = dataRows.length;
        const totalPages = Math.max(1, Math.ceil(totalRows / pageSize));
        currentPage = Math.min(Math.max(1, page), totalPages);

        if (!totalRows) {
          emptyRows.forEach((row) => {
            row.style.display = '';
          });
          info.textContent = 'Hiển thị 0 / 0 dữ liệu';
          controls.innerHTML = '';
          pageSizeWrap.style.display = 'none';
          return;
        }

        pageSizeWrap.style.display = '';

        const start = (currentPage - 1) * pageSize;
        const end = Math.min(start + pageSize, totalRows);

        dataRows.forEach((row, index) => {
          const visible = index >= start && index < end;
          row.style.display = visible ? '' : 'none';
        });
        emptyRows.forEach((row) => {
          row.style.display = 'none';
        });

        const visibleRows = dataRows.slice(start, end);
        visibleRows.forEach((row, index) => {
          const sttCell = row.querySelector('.stt-cell');
          if (sttCell) sttCell.textContent = String(start + index + 1);
        });

        info.textContent = `Hiển thị ${start + 1} - ${end} / ${totalRows} dữ liệu`;
        controls.innerHTML = '';

        const prevBtn = document.createElement('button');
        prevBtn.type = 'button';
        prevBtn.className = 'pagination-btn';
        prevBtn.textContent = 'Trước';
        prevBtn.disabled = currentPage === 1;
        prevBtn.addEventListener('click', () => renderPage(currentPage - 1));
        controls.appendChild(prevBtn);

        for (let i = 1; i <= totalPages; i++) {
          const btn = document.createElement('button');
          btn.type = 'button';
          btn.className = 'pagination-btn' + (i === currentPage ? ' active' : '');
          btn.textContent = String(i);
          btn.addEventListener('click', () => renderPage(i));
          controls.appendChild(btn);
        }

        const nextBtn = document.createElement('button');
        nextBtn.type = 'button';
        nextBtn.className = 'pagination-btn';
        nextBtn.textContent = 'Sau';
        nextBtn.disabled = currentPage === totalPages;
        nextBtn.addEventListener('click', () => renderPage(currentPage + 1));
        controls.appendChild(nextBtn);
      }

      select.addEventListener('change', () => {
        pageSize = parseInt(select.value, 10);
        localStorage.setItem(getStorageKey(table), String(pageSize));
        renderPage(1);
      });

      wrap.appendChild(left);
      wrap.appendChild(controls);
      table.closest('.table-wrap')
        ? table.closest('.table-wrap').insertAdjacentElement('afterend', wrap)
        : table.insertAdjacentElement('afterend', wrap);

      renderPage(1);
      table.dataset.paginationInitialized = 'true';
    });
  }

  document.addEventListener('DOMContentLoaded', initTablePagination);
  window.initTablePagination = initTablePagination;
})();
