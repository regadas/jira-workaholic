issue = "{{#issues}}<li><span class='label label-info issue-event' data-issue='{{ key }}' data-summary='{{ summary }}'>{{ key }}</span><p class='issue-text' data-original-title='{{summary}}'><small>{{summary}}</small></p></li>{{/issues}}"
info = "<div class='alert alert-info'>{{ message }}</div>"
warn_sync = "<div class='alert'><strong> Warning! </strong>{{ message }}<a class='btn btn-warning' href='/state/sync'>Sync with JIRA</a></div>"
issue_template = Hogan.compile(issue)
info_message = Hogan.compile(info)
warn_sync_message = Hogan.compile(warn_sync)
calendar = $('#calendar')

render_calendar = (events) ->
  calendar.empty()
  calendar.fullCalendar
      events: events
      header:
          left: 'prev,next today'
          center: 'title'
          right: 'month,agendaWeek,agendaDay'
      allDayDefault: false
      editable: true
      droppable: true
      eventDrop: (event,dayDelta,minuteDelta,allDay,revertFunc) ->
        alert(
            event.title + " was moved " +
            dayDelta + " days and " +
            minuteDelta + " minutes."
        )
        if allDay
          alert("Event is now all-day");
        else
          alert("Event has a time-of-day")
        if !confirm("Are you sure about this change?")
          revertFunc()
      eventClick: (event, jsEvent, view) ->
        #TODO: confirm dialog should be temporary
        if confirm("Delete this worklog?")
          $.post "/projects/#{event.project}/issues/#{event.issue}/worklog/delete", JSON.stringify({
            created: event.created
          }), (data) ->
            calendar.fullCalendar 'removeEvents', (object) ->
              object.created == event.created
      drop: (date, allDay) ->
          original = $(this).data 'eventObject'
          copy = $.extend {}, original
          copy.start = date
          copy.end = new Date(date)
          copy.allDay = false
          $('#myModal').data 'eventObject', copy
          $('#myModal').modal 'toggle'
          $('#notification').show()

make_droppable = (elem) ->
  project = elem.data('project')
  issue = elem.data('issue')
  elem.data 'eventObject', 
    title: $.trim(issue)
    issue: issue
    project: project
  elem.draggable
    zIndex: 999,
    revert: true,
    revertDuration: 0

check_state = () ->
  $.getJSON "/state", (data) ->
    if data.cache
      $('#messages').append warn_sync_message.render({ 
        message: "Hey! You have unsaved changes."
      })

#issues favaorite area made droppable
$('#fav-issues').droppable
  drop: (event, ui) ->
    issue_dom = ui.draggable
    $(this).find('.empty').remove()
    elems = $(this).find('li .issue-event')
    exists = () ->
      result = false
      elems.each (e) ->
        if $(this).data('issue') is issue_dom.data('issue')
          result = true
      result
    if not exists()
      rendered = $(issue_template.render
        issues:
          key: issue_dom.data('issue')
          summary: issue_dom.data('summary')
      )
      make_droppable rendered.find('.issue-event')
      $(this).append rendered


$('.issue-text').live 'hover', (e) ->
  $(this).tooltip
    placement: 'right'
  $(this).tooltip 'show'

$('.project').live 'click', (e) ->
  p = $(this)
  if !p.hasClass 'active'
    $('.active').find('ul').hide()
    $('.active').removeClass 'active'
    p.addClass 'active'
    ul = $(this).find 'ul'
    project = $(this).data('key')
    $.getJSON "/projects/#{project}/issues", (data) ->
      data.map (e) ->
        e.project = project
      ul.html(issue_template.render({ issues: data })).show()
      $('.issue-event').each () ->
        make_droppable $(this)
    
    $.getJSON "/state/#{project}/worklog", (cached) ->
      cached.map (e) ->
        e.color = 'red' 
      $.getJSON "/projects/#{project}/worklog", (data) ->
        calendar.fullCalendar 'removeEvents'
        data.concat(cached).map (e) ->
          calendar.fullCalendar 'renderEvent', e, true

# Consider using some sort of plugin here
typeTimeout = null

search = () ->
  q = $.trim $("#q").val()
  ul = $('#results').find('ul')
  ul.empty()
  if q.length > 4
    $('#search-spinner').spin 'small'
    $.post "/search/issues", {query: q }, (data) ->
      ul.append issue_template.render({ issues: data })
      ul.find('li .issue-event').each () ->
        make_droppable $(this)
      $('#search-spinner').spin false
      
$("#q").keyup (e) ->
  typeTimeout = setTimeout search, 500
  
$("#q").keydown (e) ->
  clearTimeout typeTimeout
  
$("#q-form").live 'submit', (e) ->
  e.preventDefault()
  clearTimeout typeTimeout
  search
  false

$('#add').live 'click', (e) ->
  event = $("#myModal").data 'eventObject'
  event.color = 'red'
  event.created = new Date()
  start = $("#start-event").val().split ':'
  end = $("#end-event").val().split ':'
  event.start.setHours start[0]
  event.start.setMinutes start[1]
  event.end.setHours end[0]
  event.end.setMinutes end[1]
  
  if event.end < event.start
    event.end.setDate(event.start.getDate() + 1)
  
  $.post "/projects/#{event.project}/issues/#{event.issue}/worklog", JSON.stringify({
    start: event.start.getTime()
    end: event.end.getTime()
    created: event.created.getTime()
  }), (data) ->
    calendar.fullCalendar 'renderEvent', event, true
    
  $('#myModal').modal 'toggle'
  check_state()

$("#start-event").timepicker
  timeFormat: 'G:i'
  scrollDefaultNow: true
  step: 15
  
$("#end-event").timepicker
  timeFormat: 'G:i'
  scrollDefaultNow: true
  step: 15

render_calendar []
check_state()
