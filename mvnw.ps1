param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Arguments
)

Push-Location "$PSScriptRoot\springia-rag"
try {
    & ".\mvnw.cmd" @Arguments
} finally {
    Pop-Location
}
