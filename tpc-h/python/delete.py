#main
try:
	output = open('deleteSQL.txt', 'w')
	file_object = open('delete.1')
	for line in file_object:
		items = line.split('|')
		line = 'delete from lineitem where L_ORDERKEY = ' + items[0] + '\n'
		line += 'delete from orders where O_ORDERKEY = ' + items[0] + '\n'
		#print(line, end = '')
		output.write(line)
	output.close()
	file_object.close()
except Exception as e:
	print(e)