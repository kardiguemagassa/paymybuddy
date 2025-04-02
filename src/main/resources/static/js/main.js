document.addEventListener("DOMContentLoaded", () => {
    // Gestion des devises
    const setupCurrencySelection = () => {
        const currencySelect = document.getElementById("currency");
        const amountInput = document.getElementById("amount");

        if (!currencySelect || !amountInput) return;

        const currencySymbols = {
            "EUR": "€",
            "XOF": "XOF",
            "USD": "$",
            "JPY": "¥",
            "CNY": "¥"
        };

        // Met à jour le placeholder initial
        const updatePlaceholder = () => {
            const selectedCurrency = currencySelect.value;
            amountInput.placeholder = `0 ${currencySymbols[selectedCurrency] || ''}`;
        };

        // Écoute les changements de devise
        currencySelect.addEventListener('change', updatePlaceholder);

        // Initialise au chargement
        updatePlaceholder();
    };

    // Gestion des confirmations de suppression
    const setupDeleteConfirmations = () => {
        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                if (!confirm('Voulez-vous vraiment effectuer cette suppression ?')) {
                    e.preventDefault();
                }
            });
        });
    };

    // Initialisation
    setupCurrencySelection();
    setupDeleteConfirmations();
});