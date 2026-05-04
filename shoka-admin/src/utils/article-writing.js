const CJK_PATTERN = /[\u3400-\u9fff]/g;
const WORD_PATTERN = /[A-Za-z0-9]+(?:[-_'][A-Za-z0-9]+)*/g;

export function stripArticleContent(content) {
  return String(content || "")
    .replace(/```[^\n]*\n/g, " ")
    .replace(/```/g, " ")
    .replace(/<[^>]*>/g, " ")
    .replace(/[`*_>#~\-[\]()+.!?，。；：、]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

export function getArticleWritingStats(content) {
  const plainText = stripArticleContent(content);

  if (!plainText) {
    return {
      wordCount: 0,
      readMinutes: 0,
    };
  }

  const cjkCount = (plainText.match(CJK_PATTERN) || []).length;
  const latinCount = (plainText.replace(CJK_PATTERN, " ").match(WORD_PATTERN) || []).length;
  const wordCount = cjkCount + latinCount;

  return {
    wordCount,
    readMinutes: Math.max(1, Math.ceil(wordCount / 320)),
  };
}
