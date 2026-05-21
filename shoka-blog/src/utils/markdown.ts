export const normalizeAiMarkdown = (content: string) =>
  content
    .replace(/\r\n/g, "\n")
    .replace(/([^\n])([ \t]*)(?=#{1,6}[ \t]+\S)/g, "$1\n\n");
