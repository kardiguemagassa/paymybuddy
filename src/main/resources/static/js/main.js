document.addEventListener("DOMContentLoaded",  () =>{
    updatePlaceholder();
    document.getElementById("currency").addEventListener("change", updatePlaceholder);
});

function updatePlaceholder() {
    let currencySelect = document.getElementById("currency");
    let amountInput = document.getElementById("amount");

    let currencySymbols = {
        "EUR": "€",
        "XOF": "XOF",
        "USD": "$",
        "JPY": "¥",
        "CNY": "¥"
    };

    let selectedCurrency = currencySelect.value;
    amountInput.placeholder = "0" + currencySymbols[selectedCurrency];
}
