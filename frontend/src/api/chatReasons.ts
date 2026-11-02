export interface ReportReasonOption {
  value: string
  label: string
}

/** Matches the ReportReason enum in the backend: HARASSMENT, SPAM, UNSAFE, OTHER. */
export const REPORT_REASONS: ReportReasonOption[] = [
  { value: 'HARASSMENT', label: 'Harassment or bullying' },
  { value: 'SPAM', label: 'Spam or scam' },
  { value: 'UNSAFE', label: 'I feel unsafe' },
  { value: 'OTHER', label: 'Something else' },
]
