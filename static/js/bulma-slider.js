bulmaSlider.attach();

function adjustColumns(){
  const slider = document.getElementById("columns-slider");
  let elements = document.getElementsByClassName("adjustable-column");
  for(let x=0; x < elements.length; x++){
    let columnElement = elements[x];
    columnElement.className = "adjustable-column column has-text-centered"
    if (slider.value == 5){
        columnElement.classList.add("is-one-fifth");
    }else {
        const size = Math.floor(12/slider.value);
        columnElement.classList.add(`is-${size}`);
    }
  }
}

ready(adjustColumns());
ready(function(){
  if (window.innerWidth <= 768){
    const sliderBox = document.getElementById("slider-box");
    sliderBox.parentNode.removeChild(sliderBox);
  }
});