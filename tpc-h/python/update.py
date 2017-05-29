def trans_tbl_update(table_name, char_idx, output):
	file_object = open(table_name + '.tbl.u1')
	for line in file_object:
		items = line.split('|')
		result = ''
		for i in range(0, len(items) - 1):
			if i in char_idx:
				result += '\'' + items[i] + '\''
			else:
				result += items[i]
			result += ', '
		line = 'insert into ' + table_name + ' values (' + result[:-2] + ')\n'
		#print(line, end = '')
		output.write(line)
	file_object.close()
	print(table_name + ' finished')
	return

#main
try:
	output = open('updateSQL.txt', 'w')
	trans_tbl_update('orders', [2, 4, 5, 6, 8], output)
	trans_tbl_update('lineitem', [8, 9, 10, 11, 12, 13, 14, 15], output)
	output.close()
except Exception as e:
	print(e)