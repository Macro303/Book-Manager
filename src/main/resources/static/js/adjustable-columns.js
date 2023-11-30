function changeColumnCount() {
  const settingsForm = document.getElementById("adjustable-columns-form");
  const formDetails = Object.fromEntries(new FormData(settingsForm));

  adjustColumns(formDetails["column-count"]);
}

function adjustColumns(columnCount = getCookie("bookshelf_column-count") || 3) {
  document.getElementById("column-count").value = columnCount;
  document.cookie = `bookshelf_column-count=${columnCount};path=/;max-age=${60 * 60 * 24 * 30};SameSite=Strict`;

  const elements = document.getElementsByClassName("adjustable-column");
  for (const columnElement of elements) {
    columnElement.className = "adjustable-column column";
    if (columnCount == 5)
      columnElement.classList.add("is-one-fifth");
    else {
      const size = Math.floor(12 / columnCount);
      columnElement.classList.add(`is-${size}`);
    }
  }
}

ready(() => {
  adjustColumns();
  if (window.innerWidth <= 768) {
    const sliderBox = document.getElementById("adjustable-columns");
    sliderBox?.parentNode?.removeChild(sliderBox);
  }
});
