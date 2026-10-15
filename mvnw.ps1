param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Arguments
)

& "$PSScriptRoot\mvnw.cmd" @Arguments
