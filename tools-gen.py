import json

with open('tools.json') as f:
    tools_object = json.load(f)

check = '\cmark'
wrong = r'\xmark'

tools_object = sorted(tools_object, key=lambda k: len(k['hashAlgorithms']))

for i in tools_object:
    hash_algorithms = len(i['hashAlgorithms'])
    
    scope = len(i['scope'])
    license = check  if i["license"] else wrong
    externalReferences = len(i["externalReferences"])
    canResolveTree = check  if i["canResolveTree"] else wrong
    reproducibility = check  if i["reproducibility"] else wrong

    print(f'\href{{{i["url"]}}}{{{i["name"]}}} & {i["version"]} & {i["collectionStep"]} & {hash_algorithms} & {scope} & {externalReferences} & {canResolveTree} & {reproducibility}  \\\\')
