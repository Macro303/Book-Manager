function changeColumnCount() {
    let settingsForm = document.getElementById("settings-form");
    let formDetails = Object.fromEntries(new FormData(settingsForm));
    console.log(formDetails);

    let columnCount = formDetails["column-count"];
    adjustColumns(columnCount);
}

function adjustColumns(columnCount = null) {
    if (columnCount === null)
        columnCount = getCookie("bookshelf_column-count");
    columnCount = columnCount || 3;

    let settingsForm = document.getElementById("column-count").value=columnCount;
    document.cookie = `bookshelf_column-count=${columnCount};path=/;max-age=${60*60*24*30};SameSite=Strict`;

    let elements = document.getElementsByClassName("adjustable-column");
    for (let x=0; x < elements.length; x++) {
        let columnElement = elements[x];
        columnElement.className = "adjustable-column column"
        if (columnCount == 5)
            columnElement.classList.add("is-one-fifth");
        else {
            const size = Math.floor(12/columnCount);
            columnElement.classList.add(`is-${size}`);
        }
    }
}

ready(adjustColumns());
ready(function() {
    if (window.innerWidth <= 768) {
        const sliderBox = document.getElementById("settings-box");
        sliderBox.parentNode.removeChild(sliderBox);
    }
});
