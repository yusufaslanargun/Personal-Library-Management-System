export type MediaType = 'BOOK' | 'DVD'
export type MediaStatus = 'AVAILABLE' | 'LOANED'

export interface ExternalLink {
  id: number
  provider: string
  externalId?: string
  url?: string
  rating?: number
  summary?: string
  lastSyncAt?: string
}

export interface BookInfo {
  isbn?: string
  pages?: number
  publisher?: string
  authors?: string[]
}

export interface DvdInfo {
  runtime?: number
  director?: string
  cast?: string[]
}

export interface Item {
  id: number
  type: MediaType
  title: string
  year: number
  condition?: string
  location?: string
  status: MediaStatus
  deletedAt?: string
  createdAt?: string
  updatedAt?: string
  progressPercent: number
  progressValue: number
  totalValue: number
  tags: string[]
  bookInfo?: BookInfo
  dvdInfo?: DvdInfo
  externalLinks: ExternalLink[]
}

export interface SearchResponse {
  items: Item[]
  page: number
  size: number
  total: number
}

export interface ExternalCandidate {
  provider: string
  externalId: string
  title: string
  authors?: string[]
  publisher?: string
  pageCount?: number
  year?: number
  description?: string
  infoLink?: string
  averageRating?: number
}

export interface ProgressLog {
  id: number
  date: string
  durationMinutes?: number
  pageOrMinute: number
  percent: number
}

export interface Loan {
  id: number
  itemId: number
  toWhom: string
  startDate: string
  dueDate: string
  returnedAt?: string
  status: 'ACTIVE' | 'RETURNED'
}

export interface MediaListItem {
  itemId: number
  title: string
  position: number
  priority: number
}

export interface MediaList {
  id: number
  name: string
  items: MediaListItem[]
}

export interface ImportSummary {
  added: number
  updated: number
  skipped: number
  errors: string[]
}

export interface SyncStatus {
  enabled: boolean
  lastSyncAt?: string
  lastStatus?: string
  lastConflictCount?: number
}

export interface User {
  id: number
  email: string
  displayName: string
  createdAt: string
  lastLoginAt?: string
}

export interface AuthResponse {
  token: string
  user: User
}
