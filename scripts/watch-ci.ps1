param(
    [string]$Repository = "sxd91/suchat",
    [int]$IntervalSeconds = 15,
    [int]$TimeoutMinutes = 30
)

$deadline = (Get-Date).AddMinutes($TimeoutMinutes)
while ((Get-Date) -lt $deadline) {
    $runJson = $null
    for ($attempt = 1; $attempt -le 3 -and $null -eq $runJson; $attempt++) {
        try { $runJson = gh run list --repo $Repository --limit 1 --json databaseId,status,conclusion,workflowName,url,headSha 2>$null } catch { $runJson = $null }
        if ($null -eq $runJson -or [string]::IsNullOrWhiteSpace($runJson)) {
            Write-Output "GitHub Actions query retry $attempt of 3."
            Start-Sleep -Seconds 5
            $runJson = $null
        }
    }
    if ($null -eq $runJson) { Write-Output "GitHub Actions API remained unavailable; retrying at the next poll."; Start-Sleep -Seconds $IntervalSeconds; continue }
    $run = $runJson | ConvertFrom-Json | Select-Object -First 1
    if ($null -eq $run) { Write-Output "No workflow run found for $Repository; retrying at the next poll."; Start-Sleep -Seconds $IntervalSeconds; continue }
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
