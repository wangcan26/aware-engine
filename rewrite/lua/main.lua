

function main()
	local window = ae_native.GameWindow()
	--window:testException()
	window:open('', 640, 480, false)
	
	setup2DWindow(window)
		
	--gl.ClearColor(1.0, 0, 0, 0)
	
	local x = 0
	local y = 0
	local dx = 0.01
	local dy = -0.02
	
	while (not window:haveQuitEvent()) do
		gl.Clear(gl.COLOR_BUFFER_BIT)
		
		gl.Color(1, 0, 0, 1)
		draw2DBox(x, y, 0.5, 0.5, true, true)
		
		x = x + dx
		y = y + dy
		
		if x > 1 or x < -1 then
			dx = -dx
		end
		if y > 1 or y < -1 then
			dy = -dy
		end
		
		window:swapBuffers()
	end
	
	window:close()
end

function setup2DWindow(window)
	local w = window:getWidth()
	local h = window:getHeight()
	
	gl.Viewport(0, 0, w, h)
	gl.MatrixMode(gl.PROJECTION)
	gl.LoadIdentity()
	gl.Ortho(-1, 1, -1, 1, 10, -10) --left, right, bottom, top

	gl.MatrixMode(gl.MODELVIEW)
	gl.LoadIdentity()
end

function draw2DBox(left, top, width, height, filled, center)
	if center then
		left = left - (width / 2.0)
		top = top + (height / 2.0)
	end

	if filled then
		gl.PolygonMode(gl.FRONT_AND_BACK, gl.FILL)
	else
		gl.PolygonMode(gl.FRONT_AND_BACK, gl.LINE)
	end

	gl.Begin(gl.QUADS)
	gl.TexCoord(1, 1)
	gl.Vertex(left + width, top)
	gl.TexCoord(0, 1)
	gl.Vertex(left, top)
	gl.TexCoord(0, 0)
	gl.Vertex(left, top - height)
	gl.TexCoord(1, 0)
	gl.Vertex(left + width, top - height)
	gl.End()
end

function _onerror(msg)
	print('ERROR: ' .. msg)
	print(debug.traceback())
end

-- ALWAYS LAST
xpcall(main, _onerror)
