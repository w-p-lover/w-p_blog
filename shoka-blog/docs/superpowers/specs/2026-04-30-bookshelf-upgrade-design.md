# Bookshelf Upgrade Design

Date: 2026-04-30
Project: `shoka-blog` frontend plus `blog-springboot` book service

## Goal

Upgrade the current bookshelf from a simple visual list into a practical and polished desktop-first Web book management page. The new page should feel like a personal digital study: visually warm, easy to scan, and useful for maintaining books, reading status, tags, notes, and resource links.

Mobile and narrow screens must remain usable, but the primary design target is desktop Web.

## Current Findings

- The frontend book page is concentrated in `src/views/Book/index.vue`, with display, add form, detail dialog, sorting, resource editing, and layout mutation all mixed together.
- The current visual style relies on a large page header and 3D book shapes. It has personality, but the content area is hard to scan and the controls do not scale well.
- `el-input-tag` is not available in the current Element Plus version, so the add-book tag input emits component resolution warnings.
- The book API returns usable data, including title, author, cover, status, tags, brief, brief image, add time, and JSON-encoded resources.
- `BookServiceImpl.java` uses current page list length as `count`, keeps resource JSON handling inline, and allows status/resource updates without much domain validation.

## Product Shape

The page becomes a "Book Studio" rather than a static shelf.

Core user jobs:

- Browse the whole collection quickly.
- Search by title, author, or tag.
- Filter by reading status: all, wish, reading, read.
- Sort by add time or title.
- Add a new book without fighting the form.
- Open a book and immediately manage status, tags, notes, and resource links.
- See practical collection signals, such as total books, books in progress, completed books, and resource count.

## Desktop Layout

### Header

Use a shorter atmospheric header, about 300-360px on desktop. The title remains `书架`, but the first viewport should reveal the beginning of the management area. The header should not consume the whole screen.

Header content:

- Page title and a short subtitle.
- Collection summary: total books, reading, read, wish.
- Optional visual accent using the existing background image, with darker overlay for readability.

### Toolbar

The toolbar sits directly above the book grid and is optimized for desktop scanning.

Controls:

- Search input for title, author, and tags.
- Status segmented filter: 全部, 想读, 在读, 已读.
- Sort select: newest, oldest, title ascending.
- Density/view toggle: comfortable grid and compact grid.
- Primary add-book button.

Behavior:

- Search and status filtering can be local after the list loads.
- Sort should continue to use backend-supported sort types where available.
- Toolbar must not overflow on desktop or mobile. On narrow screens, it wraps into two rows.

### Book Grid

Use a responsive desktop grid, typically 4-5 columns on wide screens.

Each book card shows:

- Cover image with a subtle dimensional hover effect.
- Title and author.
- Status badge.
- Up to three tag chips.
- Resource count.
- Add time or a compact metadata line.

The current 3D book idea can be retained as a restrained visual treatment, but the card must remain a stable information surface. Hover may lift the card and reveal quick actions, but it must not shift the grid.

Quick actions:

- Open detail.
- Change status.
- Open first resource when available.

### Empty And Error States

Empty state:

- If no books exist, show a tasteful empty shelf prompt with an add button.
- If filters produce no result, show a filtered-empty state with a clear-filter action.

Error state:

- If the backend request fails, show an inline retry surface instead of leaving the page visually empty.
- Keep console noise low for bookshelf-owned code.

## Detail And Management

Desktop detail opens as a wide dialog or side panel.

Left column:

- Cover preview.
- Status switcher.
- Basic metadata.
- Tag editor.

Right column:

- Brief/notes area.
- Resource list.
- Add/edit/delete resource actions.

Resource behavior:

- Resources have `name`, `url`, and `type`.
- URL opening should validate that a URL exists.
- The UI should clearly distinguish PDF, note, and other resources.
- Resource edits should persist through the existing API first, while leaving room for a cleaner request-body endpoint later.

Add/edit book form:

- Replace unsupported `el-input-tag` with a compatible tag-entry pattern based on `el-input`, `el-tag`, and keyboard handling.
- Validate required title before submitting.
- Preserve existing fields: title, author, status, tags, cover, brief image, brief, resource.
- Add practical defaults: status defaults to `wish`; resources default to an empty array.

## Backend Design

Scope for `BookServiceImpl.java`:

- Fix list `count` so it represents total matching rows instead of current page length.
- Extract resource parsing and serialization helpers.
- Validate status values against `wish`, `reading`, and `read`.
- Make duplicate-book checks reusable between add and update.
- Improve resource deletion errors for missing book, missing resources, and out-of-range index.
- Keep current public endpoints compatible unless a frontend change explicitly migrates them.

Optional endpoint improvement:

- Add a cleaner request-body endpoint for updating resources, while preserving the existing query-parameter endpoint for compatibility.

## Component Boundaries

Frontend files should be split enough to keep behavior understandable:

- `src/views/Book/index.vue`: page composition and data loading.
- `src/views/Book/components/BookToolbar.vue`: search/filter/sort/add controls.
- `src/views/Book/components/BookCard.vue`: single book presentation and quick actions.
- `src/views/Book/components/BookDetailDialog.vue`: detail and resource management.
- `src/views/Book/components/BookFormDialog.vue`: add/edit form.
- `src/views/Book/bookModel.ts`: local filtering, sorting, tag parsing, resource parsing helpers.
- Existing SCSS may be replaced or reorganized into page and component styles.

The exact split can be adjusted during implementation if the existing project patterns make a smaller split more natural.

## Visual Direction

Tone: refined personal study, not admin dashboard and not pure decorative 3D shelf.

Design qualities:

- Warm but not beige-dominated.
- Dark ink, muted teal, paper white, restrained amber accents.
- Book covers remain the main visual assets.
- Cards use small radii, stable dimensions, and clear typography.
- Motion is subtle: hover lift, cover sheen, and dialog transitions.
- No oversized marketing hero, no decorative orb backgrounds.

## Testing And Verification

Frontend:

- Add tests for local book filtering, tag parsing, resource parsing, and status counting.
- Build with `npm run build`.
- Verify in browser at `http://localhost:5173/book`.
- Check desktop-width layout and narrow-width degradation.

Backend:

- Add unit tests or focused service tests for status validation, duplicate checks, resource parsing/deletion, and count behavior where project test structure allows.
- If backend test setup is too heavy, verify with targeted API calls and document the limitation.

## Non-Goals

- Do not build a full reading-progress tracker with chapters/pages unless requested later.
- Do not introduce a new design system or UI library.
- Do not change unrelated blog pages except where global floating components interfere with the bookshelf layout.
- Do not break existing book endpoints used by the current frontend.

## Open Assumptions

- Desktop Web is the primary target.
- The current statuses remain `wish`, `reading`, and `read`.
- Tags can remain comma-separated in storage for now, but the frontend should treat them as arrays.
- Book resources remain JSON-backed for the first upgrade pass.
