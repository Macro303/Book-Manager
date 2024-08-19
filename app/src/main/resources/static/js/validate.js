function validateField(name, errorText, type, allowZero = false) {
  const field = document.getElementById(name);
  const fieldError = document.getElementById(`${name}-error`);

  function setFieldError() {
    field.classList.add("is-danger");
    fieldError.textContent = errorText;
    field.focus();
  }

  let fieldValue;
  switch (type) {
    case "float":
      fieldValue = parseFloat(field.value);
      if ((fieldValue === 0 && !allowZero) || isNaN(fieldValue)) {
        setFieldError();
        return false;
      }
      break;
    case "int":
      fieldValue = parseInt(field.value, 10);
      if ((fieldValue === 0 && !allowZero) || isNaN(fieldValue)) {
        setFieldError();
        return false;
      }
      break;
    default:
      fieldValue = field.value.trim();
      if (fieldValue === "") {
        setFieldError();
        return false;
      }
  }

  field.classList.remove("is-danger");
  fieldError.textContent = "";
  return true;
}

function validateText(name, errorText) {
  return validateField(name, errorText, "text");
}

function validateFloat(name, errorText, allowZero) {
  return validateField(name, errorText, "float", allowZero);
}

function validateInt(name, errorText, allowZero) {
  return validateField(name, errorText, "int", allowZero);
}

function validateSelect(name, errorText) {
  return validateField(name, errorText, "text");
}

function validateAny(options) {
  return options.some((element, index, array) => {
    const field = document.getElementById(element);
    return field.value !== "";
  });
}

async function userValidation() {
  return (
    validateText("username", "Please enter a Username.")
  )
}

async function importValidation() {
  return (
    validateAny(["goodreads", "google-books", "isbn", "library-thing", "open-library"])
  )
}
