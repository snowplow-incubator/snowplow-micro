/**
 * Sort keys with priority: unstruct_event_ first, contexts_ second, then alphabetically
 */
export function sortKeys(keys: string[]): string[] {
  return keys.sort((a, b) => {
    const aIsUnstruct = a.startsWith('unstruct_event_');
    const bIsUnstruct = b.startsWith('unstruct_event_');
    const aIsContext = a.startsWith('contexts_');
    const bIsContext = b.startsWith('contexts_');

    // Both unstruct_event_: sort alphabetically within category
    if (aIsUnstruct && bIsUnstruct) {
      return a.localeCompare(b);
    }

    // Both contexts_: sort alphabetically within category
    if (aIsContext && bIsContext) {
      return a.localeCompare(b);
    }

    // unstruct_event_ comes first
    if (aIsUnstruct && !bIsUnstruct) return -1;
    if (!aIsUnstruct && bIsUnstruct) return 1;

    // contexts_ comes second (after unstruct_event_)
    if (aIsContext && !bIsContext && !bIsUnstruct) return -1;
    if (!aIsContext && bIsContext && !aIsUnstruct) return 1;

    // Everything else: alphabetically
    return a.localeCompare(b);
  });
}