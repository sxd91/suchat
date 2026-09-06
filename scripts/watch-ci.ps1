param(
    [string]$Repository = "sxd91/suchat",
    [int]$IntervalSeconds = 15,
    [int]$TimeoutMinutes = 30
)

$deadline = (Get-Date).AddMinutes($TimeoutMinutes)
while ((Get-Date) -lt $deadline) {
    $run = gh run list --repo $Repository --limit 1 --json databaseId,status,conclusion,workflowName,url,headSha | ConvertFrom-Json | Select-Object -First 1
    if ($null -eq $run) { Write-Output "No workflow run found for $Repository"; exit 1 }
    Write-Output ("{0} | {1} | {2} | {3}" -f $run.workflowName, $run.status, $run.conclusion, $run.url)
    if ($run.status -eq "completed") {
        if ($run.conclusion -eq "success") { exit 0 }
        gh run view $run.databaseId --repo $Repository --log-failed
        exit 1
    }
    Start-Sleep -Seconds $IntervalSeconds
}
Write-Output "Timed out waiting for CI after $TimeoutMinutes minutes."
exit 2
