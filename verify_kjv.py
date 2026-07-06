import json

with open("C:/Users/John Victor/Documents/Development/AttentionPanner/assets/kjv.json", encoding="utf-8") as f:
    data = json.load(f)

books = {}
for v in data:
    book = v["reference"].split()[0]
    books[book] = books.get(book, 0) + 1

print(f"Total verses: {len(data)}")
for b in sorted(books):
    print(f"  {b}: {books[b]} verses")

print("\nFirst:")
print(json.dumps(data[0], indent=2))

print("\nLast:")
print(json.dumps(data[-1], indent=2))
