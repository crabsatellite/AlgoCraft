import json
import os
import re

# Categories
binary_tree = []
linked_list = []
graph = []
matrix_grid = []

# Analyze all problem files
for i in range(1, 501):
    filename = f'p{i}.json'
    if not os.path.exists(filename):
        continue
    
    try:
        with open(filename, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        pid = data.get('id', str(i))
        title = data.get('title', '')
        tags = data.get('tags', [])
        description = data.get('description', '')
        examples = data.get('examples', [])
        initial_code = data.get('initialCode', '')
        
        # Extract example inputs
        example_inputs = [ex.get('input', '') for ex in examples]
        
        # Check for binary tree
        is_tree = False
        if any(t in tags for t in ['Binary Tree', 'Tree', 'Binary Search Tree']):
            is_tree = True
        if 'TreeNode' in initial_code or 'TreeNode' in description:
            is_tree = True
        if re.search(r'root\s*=\s*\[', description):
            is_tree = True
        
        # Check for linked list
        is_list = False
        if 'Linked List' in tags:
            is_list = True
        if 'ListNode' in initial_code or 'ListNode' in description:
            is_list = True
        if re.search(r'head\s*=\s*\[', description):
            is_list = True
        
        # Check for graph
        is_graph = False
        if 'Graph' in tags:
            is_graph = True
        if re.search(r'(graph|edges|adjacency)\s*=\s*\[\[', description, re.IGNORECASE):
            is_graph = True
        if 'prerequisites' in description.lower() and '[[' in description:
            is_graph = True
        if re.search(r'n\s*=\s*\d+.*edges\s*=', description, re.IGNORECASE):
            is_graph = True
        
        # Check for matrix/grid
        is_matrix = False
        matrix_keywords = ['matrix', 'grid', 'board', 'maze', 'dungeon']
        if any(kw in description.lower() for kw in matrix_keywords):
            if '[[' in description:  # Has 2D array in examples
                is_matrix = True
        
        # Add to categories
        if is_tree:
            binary_tree.append({
                'id': pid,
                'title': title,
                'examples': example_inputs[:2]
            })
        if is_list:
            linked_list.append({
                'id': pid,
                'title': title,
                'examples': example_inputs[:2]
            })
        if is_graph:
            graph.append({
                'id': pid,
                'title': title,
                'examples': example_inputs[:2]
            })
        if is_matrix:
            matrix_grid.append({
                'id': pid,
                'title': title,
                'examples': example_inputs[:2]
            })
    except Exception as e:
        print(f'Error processing {filename}: {e}')

# Print results
print('=' * 80)
print(f'1. BINARY TREE PROBLEMS ({len(binary_tree)} total)')
print('=' * 80)
for p in binary_tree:
    print(f"ID: {p['id']} | {p['title']}")
    for ex in p['examples']:
        if 'root' in ex or '[' in ex:
            if len(ex) > 100:
                print(f"   Example: {ex[:100]}...")
            else:
                print(f"   Example: {ex}")

print()
print('=' * 80)
print(f'2. LINKED LIST PROBLEMS ({len(linked_list)} total)')
print('=' * 80)
for p in linked_list:
    print(f"ID: {p['id']} | {p['title']}")
    for ex in p['examples']:
        if 'head' in ex or 'list' in ex.lower() or '[' in ex:
            if len(ex) > 100:
                print(f"   Example: {ex[:100]}...")
            else:
                print(f"   Example: {ex}")

print()
print('=' * 80)
print(f'3. GRAPH PROBLEMS ({len(graph)} total)')
print('=' * 80)
for p in graph:
    print(f"ID: {p['id']} | {p['title']}")
    for ex in p['examples']:
        if len(ex) > 100:
            print(f"   Example: {ex[:100]}...")
        else:
            print(f"   Example: {ex}")

print()
print('=' * 80)
print(f'4. MATRIX/GRID PROBLEMS ({len(matrix_grid)} total)')
print('=' * 80)
for p in matrix_grid:
    print(f"ID: {p['id']} | {p['title']}")
    for ex in p['examples']:
        if len(ex) > 120:
            print(f"   Example: {ex[:120]}...")
        else:
            print(f"   Example: {ex}")
