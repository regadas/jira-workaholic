issue = "{{#issues}}<li><span class='label label-info issue-event'data-key='{{ key }}'>{{ key }}</span><p class='issue-text' data-original-title='{{summary}}'><small>{{summary}}</small></p></li>{{/issues}}"
issue_template = Hogan.compile(issue)
calendar = $('#calendar')

render_calendar = (events) ->
  calendar.empty()
  calendar.fullCalendar
      events: events
      header:
          left: 'prev,next today'
          center: 'title'
          right: 'month,agendaWeek,agendaDay'
      editable: true
      droppable: true
      select: (start, end, allDay) ->
        alert "LOL"
      drop: (date, allDay) ->
          original = $(this).data 'eventObject'
          copy = $.extend {}, original
          copy.start = date
          copy.end = new Date(date)
          copy.allDay = false
          $('#myModal').data 'eventObject', copy
          $('#myModal').modal 'toggle'

make_droppable = (elems) ->
  elems.each () ->
    $(this).data 'eventObject', title: $.trim($(this).text())
    $(this).draggable
      zIndex: 999,
      revert: true,
      revertDuration: 0

$('.issue-text').live 'hover', (e) ->
  $(this).tooltip 'show'

$('.project').live 'click', (e) ->
  $('.active-issues').hide()
  $('.active-issues').removeClass 'active-issues'
  ul = $(this).find 'ul'
  
  $.getJSON "/projects/#{$(this).data('key')}/issues", (data) ->
    ul.addClass('active-issues').html(issue_template.render({ issues: data })).show()
    make_droppable $('.issue-event')
    
  $.getJSON "/projects/#{$(this).data('key')}/worklog", (data) ->
    render_calendar data
    
$('#add').live 'click', (e) ->
  event = $("#myModal").data 'eventObject'
  start = $("#start-event").val().split ':'
  end = $("#end-event").val().split ':'
  event.start.setHours start[0]
  event.start.setMinutes start[1]
  event.end.setHours end[0]
  event.end.setMinutes end[1]
  
  if event.end < event.start
    event.end.setDate(event.start.getDate() + 1)
  
  $.post "/issues/#{event.title}/worklog", {
    start: event.start.toJSON()
    end: event.end.toJSON()
  }, (data) ->
    calendar.fullCalendar 'renderEvent', event, true
    true

$("#start-event").timepicker
  timeFormat: 'G:i'
  scrollDefaultNow: true
  step: 15
$("#end-event").timepicker
  timeFormat: 'G:i'
  scrollDefaultNow: true
  step: 15
render_calendar([])

  


