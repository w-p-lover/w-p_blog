# Homepage Magazine Flow Design

## Goal

Optimize the home page into a refined, ornate personal blog experience where the article list is the primary focus. The result should feel polished and expressive, but not visually noisy.

## Direction

Use a "refined alternating magazine flow" as the main design direction.

The home page should keep the existing personality: full-screen visual opening, animated brand text, talk strip, recommended article carousel, alternating article cards, and sidebar. The redesign should not remove the user's preferred left-right alternating article layout. Instead, it should make that alternating rhythm the main decorative gesture, while reducing competing visual effects elsewhere.

## Design Principles

- Keep the page ornate, but give it one visual melody.
- Make article cards the strongest content element after the hero.
- Treat the talk strip, recommendation carousel, filters, and sidebar as supporting modules.
- Reduce simultaneous use of strong gradients, heavy shadows, sharp clipping, saturated colors, and decorative typography.
- Preserve the Shoka-inspired personality with softer polish rather than minimalism.
- Prefer style and layout refinements over broad component rewrites.

## Page Structure

The home page keeps the current content order:

1. Full-screen background image carousel.
2. Brand title and typed sentence.
3. Wave transition and scroll cue.
4. Talk strip.
5. Recommended article carousel.
6. Article filter toolbar.
7. Alternating article list.
8. Pagination.
9. Right sidebar on desktop.

This order supports the desired reading flow: atmosphere first, then featured content, then articles as the main experience.

## Hero Section

The hero should remain visually rich and personal.

Expected behavior:

- Keep the full-screen image rotation.
- Keep the site title, typed sentence, wave, and scroll cue.
- Use a more consistent overlay so different background images do not create sudden contrast or color shifts.
- Keep the title legible and elegant with softer text shadow.
- Avoid adding extra content to the first viewport.

The hero's job is to create memory and mood, not to compete with the article list below.

## Content Area

The content area should feel more composed.

Expected behavior:

- Use a stable page width and spacing rhythm.
- Keep the desktop two-column structure with articles on the left and sidebar on the right.
- Let the left article column visually dominate.
- Keep the background soft and layered, but avoid multiple attention-grabbing color fields.
- Make repeated cards use a shared surface, border, radius, and shadow language.

## Talk Strip

The talk strip should become a light personal signal.

Expected behavior:

- Keep the vertical swiper behavior.
- Keep the link to the talk page.
- Reduce visual weight compared with article cards.
- Use smaller shadow and calmer border.
- Keep the icon and arrow, but avoid strong animation or color emphasis.

## Recommended Articles

The recommendation carousel should remain, but should not overpower the article list.

Expected behavior:

- Keep the image carousel and article links.
- Keep the "recommended" identity, but make the badge smaller and more refined.
- Use a consistent overlay similar to the hero.
- Reduce overly strong shadows and saturated accents.
- Keep pagination and navigation controls readable but subtle.

## Article Filter Toolbar

The filter controls should read as one toolbar instead of separate scattered controls.

Expected behavior:

- Group sorting, tag selection, and date range into a single aligned toolbar.
- Use consistent control height, radius, border, and color.
- Keep active sort state clear.
- Reduce bright hover fills and high-contrast borders.
- On mobile, stack controls cleanly without overflow.

## Article Cards

The article cards are the centerpiece.

Expected behavior:

- Keep the left-right alternating image layout.
- Keep large cover images.
- Soften the clipped image angle so the card feels elegant instead of jagged.
- Use consistent card height, radius, border, and shadow.
- Keep hover effects limited to slight lift, subtle border highlight, and gentle cover image scale.
- Make article title the strongest text inside the card.
- Make date, tags, category, and excerpt lighter.
- Use a readable body font for excerpts.
- Replace the visually loud `more...` button treatment with a smaller refined reading action.
- Prevent text overflow and cramped metadata lines on narrower screens.

The alternating layout is the main ornament. Other effects should support it rather than compete with it.

## Sidebar

The sidebar should support the personal-space feeling without becoming a second main column.

Expected behavior:

- Keep author, notice, recent comments, and web info.
- Use the same shared card surface language as the rest of the page.
- Reduce heavy shadows.
- Keep spacing calm and consistent.
- Keep the sidebar sticky on desktop.
- Hide it on smaller screens as currently implemented.

## Color System

The home page should use a restrained ornate palette.

Recommended palette behavior:

- Main accent: soft pink-orange, inherited from the existing Shoka style.
- Supporting accent: small amounts of blue-cyan for links, carousel state, or highlights.
- Surfaces: translucent white or dark-mode equivalents.
- Text: higher contrast for article titles and readable excerpts.
- Gradients: reserved for small emphasis states only.

The page should avoid showing many saturated accent colors at the same time.

## Dark Mode

Dark mode should preserve the same hierarchy.

Expected behavior:

- Keep surfaces translucent but readable.
- Avoid making all images too dim.
- Ensure article titles and metadata have clear contrast.
- Keep shadows subtle and avoid muddy low-contrast cards.

## Implementation Scope

Likely files to modify:

- `src/views/Home/index.vue`
- `src/views/Home/Brand/index.vue`
- `src/views/Home/Swiper/Images.vue`
- `src/views/Home/Swiper/TalkSwiper.vue`
- `src/views/Home/Swiper/Recommend.vue`
- `src/views/Article/ArticleItem.vue`
- `src/assets/styles/common.scss`
- `src/assets/styles/theme-shoka.scss`

Implementation should avoid changing data fetching, routing, article query behavior, or API contracts unless a layout issue requires a small template adjustment.

## Testing And Verification

Verification should include:

- Run the production build.
- Check desktop layout around 1440px width.
- Check tablet/mobile layout around 991px and 767px breakpoints.
- Verify the article list remains readable with long titles, many tags, and missing or slow-loading images.
- Verify light and dark themes.
- Verify no unrelated page styles regress from shared style variable changes.

## Out Of Scope

- Rewriting the home page as a new design system.
- Removing the alternating article layout.
- Changing backend APIs.
- Adding new homepage content modules.
- Replacing the current carousel libraries.
- Redesigning all non-home pages.
