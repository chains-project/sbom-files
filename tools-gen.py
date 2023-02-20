import json

with open('tools.json') as f:
    tools_object = json.load(f)

check = '\cmark'
wrong = r'\xmark'

tools_object = sorted(tools_object, key=lambda k: len(k['hashAlgorithms']))

for i in tools_object:
    hash_algorithms = ','.join(i['hashAlgorithms'])
    if hash_algorithms == '':
        hash_algorithms = None
    
    scope = check  if i["scope"] else wrong
    license = check  if i["license"] else wrong
    externalReferences = check  if i["externalReferences"] else wrong

    print(f'\href{{{i["url"]}}}{{{i["name"]}}} & {i["version"]} & {i["collectionStep"]} & {hash_algorithms} & {scope} & {license} & {externalReferences}  \\\\')
