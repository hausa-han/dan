fi = open("bootstrap.min.css",'r')
fo = open("fo.css", 'w')

si = fi.read()
so = ''
for i in range(0,len(si)):
	if si[i] != '}':
		so = so + si[i]
	if si[i] == '}':
		so = so + si[i] + '\n'
print(so)
fo.write(so)
