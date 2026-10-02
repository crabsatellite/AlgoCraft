import sys
from PIL import Image
names = sys.argv[2].split(',')
out = sys.argv[1]
ims = [Image.open(f'out/stills/{n}.png').convert('RGB').resize((960, 540)) for n in names]
cols = 2; rows = (len(ims) + 1) // 2
sheet = Image.new('RGB', (cols * 960 + 10, rows * 540 + (rows - 1) * 10), (255, 0, 255))
for i, im in enumerate(ims):
    sheet.paste(im, ((i % 2) * 970, (i // 2) * 550))
sheet.save(out)
